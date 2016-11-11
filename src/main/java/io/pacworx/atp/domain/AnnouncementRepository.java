package io.pacworx.atp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query(value = "SELECT * FROM announcement WHERE countries = 'ALL' order by send_date DESC limit 5", nativeQuery = true)
    List<Announcement> findGlobalAnnouncements();

    @Query(value = "SELECT * FROM announcement WHERE countries = 'ALL' or countries LIKE %:country% order by send_date DESC limit 5", nativeQuery = true)
    List<Announcement> findAnnouncementsByCountry(@Param("country") String country);

    default List<Announcement> findAnnouncements(String country) {
        if(country != null) {
            return findAnnouncementsByCountry(country);
        } else {
            return findGlobalAnnouncements();
        }
    }

    List<Announcement> findByOrderBySendDateDesc();
}
