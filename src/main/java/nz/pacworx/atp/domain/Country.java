package nz.pacworx.atp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Country {
    @Id
    private String alpha3;
    private String nameEng;
    private boolean active;

    public String getAlpha3() {
        return alpha3;
    }

    public String getNameEng() {
        return nameEng;
    }

    @JsonIgnore
    public boolean isActive() {
        return active;
    }
}
