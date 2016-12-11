package io.pacworx.atp.version;

import io.pacworx.atp.exception.AtpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/version")
public class VersionController {

    private Version version;

    @Value("${api.version}")
    public void setVersion(String version) {
        String[] split = version.split("\\.");
        this.version = new Version();
        this.version.setMajor(Integer.parseInt(split[0]));
        this.version.setMinor(Integer.parseInt(split[1]));
        this.version.setPatch(Integer.parseInt(split[2]));
    }

    @RequestMapping(value = "/check", method = RequestMethod.POST)
    public ResponseEntity<VersionResponse> checkVersion(@RequestBody Version request) {
        boolean valid = true;
        if(request.getMajor() < this.version.getMajor() || request.getMinor() < this.version.getMinor()) {
            valid = false;
        }
        return new ResponseEntity<>(new VersionResponse(valid), HttpStatus.OK);
    }

    class VersionResponse {
        public boolean success;

        public VersionResponse(boolean success) {
            this.success = success;
        }
    }
}
