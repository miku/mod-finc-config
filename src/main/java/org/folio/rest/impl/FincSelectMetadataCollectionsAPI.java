package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.finc.dao.IsilDAO;
import org.folio.finc.dao.IsilDAOImpl;
import org.folio.finc.dao.SelectMetadataCollectionsDAO;
import org.folio.finc.dao.SelectMetadataCollectionsDAOImpl;
import org.folio.finc.select.SelectMetadataCollectionsHelper;
import org.folio.finc.select.exception.FincSelectNotPermittedException;
import org.folio.rest.RestVerticle;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollection;
import org.folio.rest.jaxrs.model.FincSelectMetadataCollectionsGetOrder;
import org.folio.rest.jaxrs.model.Select;
import org.folio.rest.jaxrs.resource.FincSelectMetadataCollections;
import org.folio.rest.tools.utils.TenantTool;

public class FincSelectMetadataCollectionsAPI implements FincSelectMetadataCollections {

  private final SelectMetadataCollectionsHelper selectMetadataCollectionsHelper;
  private final SelectMetadataCollectionsDAO selectMetadataCollectionsDAO;
  private final IsilDAO isilDAO;

  public FincSelectMetadataCollectionsAPI(Vertx vertx, String tenantId) {
    this.selectMetadataCollectionsHelper = new SelectMetadataCollectionsHelper(vertx, tenantId);
    this.selectMetadataCollectionsDAO = new SelectMetadataCollectionsDAOImpl();
    this.isilDAO = new IsilDAOImpl();
  }

  @Override
  @Validate
  public void getFincSelectMetadataCollections(
      String query,
      String orderBy,
      FincSelectMetadataCollectionsGetOrder order,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
      TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO
        .getIsilForTenant(tenantId, vertxContext)
        .compose(
            isil -> selectMetadataCollectionsDAO.getAll(query, offset, limit, isil, vertxContext))
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                org.folio.rest.jaxrs.model.FincSelectMetadataCollections metadataCollections =
                    ar.result();
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        GetFincSelectMetadataCollectionsResponse.respond200WithApplicationJson(
                            metadataCollections)));
              } else {
                if (ar.cause() instanceof FieldException) {
                  Future.succeededFuture(
                    GetFincSelectMetadataCollectionsResponse.respond400WithTextPlain(ar.cause()));
                } else {
                  Future.succeededFuture(
                      GetFincSelectMetadataCollectionsResponse.respond500WithTextPlain(ar.cause()));
                }
              }
            });
  }

  @Override
  @Validate
  public void postFincSelectMetadataCollections(
      String lang,
      FincSelectMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    // Posting metadata collections is not allowed via finc-select
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections.PostFincSelectMetadataCollectionsResponse.status(
                            501)
                        .build())));
  }

  @Override
  @Validate
  public void getFincSelectMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    String tenantId =
      TenantTool.calculateTenantId(okapiHeaders.get(RestVerticle.OKAPI_HEADER_TENANT));

    isilDAO.getIsilForTenant(tenantId, vertxContext)
      .compose(isil -> selectMetadataCollectionsDAO.getById(id, isil, vertxContext))
      .setHandler(ar -> {
        if (ar.succeeded()) {
        asyncResultHandler.handle(
            Future.succeededFuture(GetFincSelectMetadataCollectionsByIdResponse.respond200WithApplicationJson(ar.result()))
          );
        } else {
          Future.succeededFuture(GetFincSelectMetadataCollectionsByIdResponse.respond500WithTextPlain(ar.cause()));
        }
      });
  }

  @Override
  @Validate
  public void deleteFincSelectMetadataCollectionsById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections
                        .DeleteFincSelectMetadataCollectionsSelectByIdResponse.status(501)
                        .build())));
  }

  @Override
  @Validate
  public void putFincSelectMetadataCollectionsById(
      String id,
      String lang,
      FincSelectMetadataCollection entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections.PutFincSelectMetadataCollectionsByIdResponse
                        .status(501)
                        .build())));
  }

  @Override
  @Validate
  public void putFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Select entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    selectMetadataCollectionsHelper
        .selectMetadataCollection(id, entity, okapiHeaders, vertxContext)
        .setHandler(
            ar -> {
              if (ar.succeeded()) {
                asyncResultHandler.handle(
                    Future.succeededFuture(
                        PutFincSelectMetadataCollectionsSelectByIdResponse.respond204()));
              } else {
                Throwable cause = ar.cause();
                if (cause instanceof FincSelectNotPermittedException) {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                          PutFincSelectMetadataCollectionsSelectByIdResponse
                              .respond404WithTextPlain("Not permitted")));
                } else {
                  asyncResultHandler.handle(
                      Future.succeededFuture(
                        PutFincSelectMetadataCollectionsSelectByIdResponse
                              .respond500WithTextPlain(ar.cause())));
                }
              }
            });
  }

  @Override
  @Validate
  public void getFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections.GetFincSelectMetadataCollectionsSelectByIdResponse
                        .status(501)
                        .build())));
  }

  @Override
  @Validate
  public void deleteFincSelectMetadataCollectionsSelectById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    vertxContext.runOnContext(
        aVoid ->
            asyncResultHandler.handle(
                succeededFuture(
                    FincSelectMetadataCollections
                        .DeleteFincSelectMetadataCollectionsSelectByIdResponse.status(501)
                        .build())));
  }
}
