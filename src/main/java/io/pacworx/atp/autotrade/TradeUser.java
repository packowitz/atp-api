package io.pacworx.atp.autotrade;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.security.MessageDigest;

@Entity
public class TradeUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    private String username;
    @JsonIgnore
    private String password;

    public void setPassword(String password) throws Exception {
        this.password = getHash(password);
    }

    public boolean passwordMatches(String password) throws Exception {
        return this.password != null && this.password.equals(getHash(password));
    }

    private String getHash(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] array = md.digest((username + "jlvjsdkfjcmskl43rf" + password + "cdscnkscnkjsac893yhi").getBytes());
        String string = "";
        for(byte aByte : array) {
            string += Integer.toHexString((aByte & 0xFF) | 0x100).substring(1,3);
        }
        return string;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
}
