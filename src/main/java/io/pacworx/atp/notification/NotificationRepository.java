package io.pacworx.atp.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Notification findByIdUserIdAndIdDeviceId(long userId, String deviceId);
}
