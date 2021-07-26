package com.gdnse.blog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication

public class ServerApplication {

    static final String DB_URL = "jdbc:mysql://localhost:3306/spring-web-blog";
    static final String USER = "root";
    static final String PASS = "";

    public static void main(String[] args) throws IOException {
        SpringApplication.run(ServerApplication.class, args);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement();
        ) {
            stmt.executeUpdate("TRUNCATE USD");
            stmt.executeUpdate("TRUNCATE EUR");
            stmt.executeUpdate("TRUNCATE RUB");

            PreparedStatement usd = conn.prepareStatement("INSERT INTO USD (date, cost, day_name) VALUES (?, ?, ?)");
            PreparedStatement rub = conn.prepareStatement("INSERT INTO RUB (date, cost, day_name) VALUES (?, ?, ?)");
            PreparedStatement eur = conn.prepareStatement("INSERT INTO EUR (date, cost, day_name) VALUES (?, ?, ?)");

            String result = "";
            List<Document> documentList = new ArrayList<>();
            documentList.add(Jsoup.connect("https://ru.exchange-rates.org/history/KZT/USD/T/").get());
            documentList.add(Jsoup.connect("https://ru.exchange-rates.org/history/KZT/RUB/T/").get());
            documentList.add(Jsoup.connect("https://ru.exchange-rates.org/history/KZT/EUR/T/").get());

            int curName = 1;
            for (Document document : documentList) {

                int day = 0;
                for (
                        Element table : document.select("table[class=table table-striped table-hover table-hover-solid-row table-simple history-data]")) {
                    for (Element row : table.select("tr")) {
                        Elements tds = row.select("td");

                        if (tds.isEmpty()) { // Header <tr> with only <th>s
                            continue;
                        }

                        System.out.println(tds.get(0).text() + "->" + tds.get(1).text() + "->" + tds.get(2).text() + "->" + tds.get(3).text());
                        result += "<b>" + tds.get(0).text() + "</b>" + " - " + tds.get(2).text() + '\n' + tds.get(1).text() + '\n' + '\n';

                        if (curName == 1) {
                            usd.setString(1, tds.get(0).text());
                            usd.setString(2, tds.get(2).text());
                            usd.setString(3, tds.get(1).text());
                            usd.executeUpdate();
                        } else if (curName == 2) {
                            rub.setString(1, tds.get(0).text());
                            rub.setString(2, tds.get(2).text());
                            rub.setString(3, tds.get(1).text());
                            rub.executeUpdate();
                        } else {
                            eur.setString(1, tds.get(0).text());
                            eur.setString(2, tds.get(2).text());
                            eur.setString(3, tds.get(1).text());
                            eur.executeUpdate();
                        }

                        day++;
                        if (day == 10) {
                            break;
                        }
                    }
                }
                curName++;
                result = "";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
