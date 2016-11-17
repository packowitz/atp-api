package io.pacworx.atp.repositories;


import io.pacworx.atp.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CountryRepository extends JpaRepository<Country, String> {
    public List<Country> findByActiveTrueOrderByNameEngAsc();
}
