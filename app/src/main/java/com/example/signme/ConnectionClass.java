package com.example.signme;

import android.media.audiofx.DynamicsProcessing;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;

public class ConnectionClass {
    protected static String db = "tsr_system3";

    protected static String ip = "10.0.2.2";

    protected static String port = "3308";

    protected static String username = "root";

    protected static String password = "6968";

    public Connection CONN() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String connectionString = "jdbc:mysql://" + ip + ":" + port + "/" + db;
            conn = DriverManager.getConnection(connectionString, username, password);
        } catch (Exception e) {
            Log.e("ERRO", Objects.requireNonNull(e.getMessage()));
        }
        return conn;
    }
}