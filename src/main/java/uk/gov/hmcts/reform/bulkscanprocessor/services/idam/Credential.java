package uk.gov.hmcts.reform.bulkscanprocessor.services.idam;

/**
 * Represents a user's credentials.
 */
public class Credential {
    private final String username;
    private final String password;

    /**
     * Creates a new instance of the class.
     * @param username the username
     * @param password the password
     */
    public Credential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the username.
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password.
     * @return the password
     */
    public String getPassword() {
        return password;
    }
}
