package org.folio.finc.select;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes query made to finc-select metadata sources. Translates selected=(yes|no) and
 * permitted=(yes|no) to selectedBy resp. permittedBy queries with corresponding isil. E.g., if
 * library with isil 'ISIL-01' looks for selected metadata collections the query 'selected="yes"'
 * will be translated to 'selectedBy="ISIL-01"'.
 */
public class QueryTranslator {

  private static final String PERMITTED = "permitted";
  private static final String SELECTED = "selected";
  private static final String YES = "yes";
  private static final String CQL_ALL_RECORDS_1_NOT = "cql.allRecords=1 NOT";
  private static final String AND = "AND";

  private QueryTranslator() {
    throw new IllegalStateException("Utility class");
  }

  public static String translate(String query, String isil) {

    if (query == null || "".equals(query)) {
      return query;
    }

    String permitted = "";
    String selected = "";
    String q = "";
    String[] ands = query.split(AND);
    for (String s : ands) {
      if (s.contains(PERMITTED)) {
        permitted = processPermittedQuery(s, isil);
      } else if (s.contains(SELECTED)) {
        selected = processSelectedQuery(s, isil);
      } else {
        String tmp = processRemainingQuery(s);
        q += calculateAppendable(q, tmp);
      }
    }

    String result = q;
    result += calculateAppendable(result, selected);
    result += calculateAppendable(result, permitted);
    return result;
  }

  private static String processSelectedQuery(String query, String isil) {
    return translate(query, SELECTED, isil, QueryTranslator::selectedBy);
  }

  private static String processPermittedQuery(String query, String isil) {
    return translate(query, PERMITTED, isil, QueryTranslator::permittedFor);
  }

  private static String processRemainingQuery(String query) {
    if ("".equals(query)) {
      return "";
    } else {
      return "(" + query.trim() + ")";
    }
  }

  private static String selectedBy(String isil) {
    return String.format("selectedBy any \"%s\"", isil);
  }

  private static String permittedFor(String isil) {
    return String.format("permittedFor any \"%s\"", isil);
  }

  private static String translate(
      String query, String key, String isil, Function<String, String> replaceQueryFunc) {

    query = prepareQuery(query);

    if (!query.contains(key)) {
      return query;
    }

    Pattern pattern =
        Pattern.compile(
            key
                + "=\\(?(\")?(?<first>[Yy][Ee][Ss]|[Nn][Oo])(\")?(\\s?(?<second>[Aa][Nn][Dd]|[Oo][Rr])\\s?(\")?(?<third>[Yy][Ee][Ss]|[Nn][Oo])(\")?)?\\)?",
            Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(query);
    if (matcher.find()) {
      String firstYesNo = matcher.group("first");
      String multiValAndOr = matcher.group("second");
      String secondYesNo = matcher.group("third");
      String group = matcher.group();
      String replacedQuery = replaceQueryFunc.apply(isil);

      if (YES.equals(firstYesNo)) {
        query = query.replace(group, replacedQuery);
      } else { // selectedValue is false
        query = query.replace(group, CQL_ALL_RECORDS_1_NOT + " " + replacedQuery);
      }

      if (multiValAndOr != null) {
        query = "(" + query + ")" + " " + multiValAndOr.toUpperCase();
        if (YES.equals(secondYesNo)) {
          query = query + " (" + replacedQuery + ")";
        } else {
          query = query + " (" + CQL_ALL_RECORDS_1_NOT + " " + replacedQuery + ")";
        }
      }
    }
    return "(" + query + ")";
  }

  private static String prepareQuery(String query) {
    query = query.trim();
    int leadingParenthesesIndex = query.indexOf('(');
    int trialingParenthesesIndex = query.indexOf(')');
    if (leadingParenthesesIndex == 0) {
      query = query.substring(1);
    }
    if (trialingParenthesesIndex == query.length() - 1) {
      query = query.substring(0, query.length() - 1);
    }
    return query;
  }

  private static String calculateAppendable(String query, String toAppend) {
    if ("".equals(query) || "".equals(toAppend)) {
      return toAppend;
    } else {
      return " " + AND + " " + toAppend;
    }
  }
}
