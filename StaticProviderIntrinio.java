/**
 * INTRINIO Data Source plug-in for ContangoTrade software
 *
 * Extra libraries:
 *   - minimal-json.jar
 *   - contangoAPI.jar
 */

package contangoStaticProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;

import contangoAPI.api.ABaseStaticProvider;
import contangoAPI.api.Bar;
import contangoStaticProvider.json.Json;
import contangoStaticProvider.json.JsonArray;
import contangoStaticProvider.json.JsonValue;

public class StaticProviderIntrinio extends ABaseStaticProvider {

  private static final String KEY = "username";
  private static final String PASS = "password";

  @Override
  public ArrayList<Bar> getData(String symbol, LocalDateTime ldt1, LocalDateTime ldt2, LocalTime lt1, LocalTime lt2,
      int timeframe) {
    
    int totalPages = 1, resultCount = 1, barCount = 0;
    HttpURLConnection conn = null;
    ArrayList<Bar> dataItems = new ArrayList<Bar>();
    String strUrl = "";

    for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
      try {
        strUrl = getUrl(symbol, dt2String(ldt1), dt2String(ldt2), pageNum);
        URL url = new URL(strUrl);
        
        String authentication = getParameter(KEY) + ":" + getParameter(PASS);
        String encoded = Base64.getEncoder().encodeToString(authentication.getBytes(StandardCharsets.UTF_8));

        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + encoded);
        conn.setConnectTimeout(120000);
        conn.setReadTimeout(120000);
        conn.connect();
        
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
          //String txt = in.lines().collect(Collectors.joining("\n"));
          String txt = readAll(in);
          if (txt.isEmpty())
            throw new ParseException("Request result is empty!", 0);

          final DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

          JsonValue value = Json.parse(txt);
          JsonArray items = value.asObject().get("data").asArray();
          totalPages = value.asObject().getInt("total_pages", 1);
          resultCount = value.asObject().getInt("result_count", 1);

          for (JsonValue item : items) {
            if (barCount >= resultCount)
              return dataItems;
            dataItems.add(new Bar(
                item.asObject().getDouble("open", 0.0),
                item.asObject().getDouble("high", 0.0),
                item.asObject().getDouble("low", 0.0),
                item.asObject().getDouble("close", 0.0),
                item.asObject().getDouble("volune", 0.0),
                df.parse(item.asObject().getString("date", ""))
                ));
            barCount++;
          }
        } // try BufferedReader
        
      } catch (ParseException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(String.format("URL problem: %s", strUrl), e);
      } finally {
        if (conn != null) {
          conn.disconnect();
        }
      }
    }
    return dataItems;
  }
  
  /**
   * Get date as string
   * @param ldt local date time
   * @return date as string
   */
  private static String dt2String(LocalDateTime ldt) {
    return String.format("%04d-%02d-%02d",
        ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth());
  }
  
  /**
   * Prepare URL string for the data provider
   * @param sym: symbol
   * @param szDT1: start date as string
   * @param szDT2: end date as string
   * @param page: page number
   * @return URL string
   */
  private static String getUrl(String sym, String szDT1, String szDT2, int page) {
    StringBuilder buf = new StringBuilder();
    buf.append("https://api.intrinio.com/prices?");
    buf.append("identifier=").append(sym);
    buf.append("&start_date=").append(szDT1);
    buf.append("&end_date=").append(szDT2);
    buf.append("&page_size=10000");
    buf.append("&page_number=").append(String.valueOf(page));
    return buf.toString();
  }
  
  /**
   * Read data
   * @param rd Reader object
   * @return read string
   * @throws IOException
   */
  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  @Override
  public String getDescription() {
    return "Intrinio data source (DAILY)";
  }

  @Override
  public void load() {
    this.addParameter(KEY, "");
    this.addParameter(PASS, "");
  }

  @Override
  public void unload() {

  }

}
