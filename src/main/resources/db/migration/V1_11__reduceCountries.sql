UPDATE country SET active = false;
UPDATE country SET active = true WHERE alpha3 IN ('DEU', 'NZL', 'AUS', 'AUT');
