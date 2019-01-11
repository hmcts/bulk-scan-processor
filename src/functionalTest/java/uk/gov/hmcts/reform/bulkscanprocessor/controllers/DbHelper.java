package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.typesafe.config.Config;
import org.postgresql.Driver;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ErrorNotification;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class DbHelper {

    private final Supplier<Connection> connectionSupplier;

    DbHelper(Config config) throws ClassNotFoundException {
        Class.forName(Driver.class.getName());

        String url = buildConnectionUrl(config);
        String username = config.getString("username");
        String password = config.getString("password");

        connectionSupplier = () -> {
            try {
                return DriverManager.getConnection(url, username, password);
            } catch (SQLException exception) {
                throw new RuntimeException(
                    "Failed to get DB connection. Message: " + exception.getMessage(),
                    exception
                );
            }
        };
    }

    private String buildConnectionUrl(Config config) {
        String host = config.getString("host");
        int port = Integer.valueOf(config.getString("port"));
        String name = config.getString("name");
        String connOptions = config.getString("conn-options");

        return String.format("jdbc:postgresql://%s:%d/%s%s", host, port, name, connOptions);
    }

    List<ErrorNotification> getErrorNotifications() throws SQLException {
        try (Connection connection = connectionSupplier.get()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM error_notifications");

            List<ErrorNotification> errorNotifications = new ArrayList<>();

            while (resultSet.next()) {
                ErrorNotification errorNotification = new ErrorNotification(
                    null,
                    resultSet.getString("errorcode")
                );
                errorNotification.setNotificationId(resultSet.getString("notificationid"));

                System.out.println("RECORD " + resultSet.getRow());
                System.out.println("ERROR CODE: " + errorNotification.getErrorCode());
                System.out.println("NOTIFICATION ID: " + errorNotification.getNotificationId());

                errorNotifications.add(errorNotification);
            }

            return errorNotifications;
        }
    }
}
