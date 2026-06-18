package com.codeguardian;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@RestController
public class TestController {

    // Hardcoded password and SQL injection vulnerability for validation testing
    private static final String DB_PASS = "admin12345";

    @GetMapping("/test-inject")
    public String checkUser(@RequestParam String name) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", DB_PASS);
            Statement stmt = conn.createStatement();
            // Classic SQL Injection
            String query = "SELECT * FROM users WHERE username = '" + name + "'";
            stmt.executeQuery(query);
            return "Executed";
        } catch (Exception e) {
            return "Error";
        }
    }
}
