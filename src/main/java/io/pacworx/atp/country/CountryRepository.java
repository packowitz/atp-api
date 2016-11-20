package io.pacworx.atp.country;


import io.pacworx.atp.country.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CountryRepository extends JpaRepository<Country, String> {
    public List<Country> findByActiveTrueOrderByNameEngAsc();
}
