package uk.gov.hmcts.reform.bulkscanprocessor.services.idam;

import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NoUserConfiguredException;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.util.Map.Entry;
import static java.util.stream.Collectors.toMap;

@ConfigurationProperties(prefix = "idam")
public class JurisdictionToUserMapping {

    private Map<String, Credential> users = new HashMap<>();

    public void setUsers(Map<String, Map<String, String>> users) {
        this.users = users
            .entrySet()
            .stream()
            .map(this::createEntry)
            .collect(toMap(Entry::getKey, Entry::getValue));
    }

    public Map<String, Credential> getUsers() {
        return users;
    }

    private Entry<String, Credential> createEntry(Entry<String, Map<String, String>> entry) {
        String key = entry.getKey().toLowerCase(Locale.getDefault());
        Credential cred = new Credential(entry.getValue().get("username"), entry.getValue().get("password"));

        return new AbstractMap.SimpleEntry<>(key, cred);
    }

    public Credential getUser(String jurisdiction) {
        return users.computeIfAbsent(jurisdiction.toLowerCase(), this::throwNotFound);
    }

    private Credential throwNotFound(String jurisdiction) {
        throw new NoUserConfiguredException(jurisdiction);
    }
}
