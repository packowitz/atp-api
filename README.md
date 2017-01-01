# atp-api

### Running locally with database

1. Install postgresql `$ sudo apt install postgresql` (Ubuntu) or `$ brew install postgres` (Mac OS)
2. Create apt user
```
$ sudo su postgres
$ psql
> CREATE ROLE atpuser WITH LOGIN PASSWORD 'askthepeople';
> \q
```

Create atp db `$ createdb -O atpuser atp`

### Links:

* [Application metric endpoints](http://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html)
* [Swagger-UI](http://localhost:8080/swagger-ui.html) (once running locally)
