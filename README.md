![](https://github.com/dump247/jupiter-db/workflows/Build%20master%20branch/badge.svg)
![](https://img.shields.io/maven-central/v/com.mostlycertain/jupiter-db-jdbc)

# Jupiter DB Test

Test extension that executes each test within a database transaction. Changes to test data are
rolled back after the test completes. This helps isolate the tests from each other.

## Usage

Add a dependency:

```groovy
testCompile "com.mostlycertain:jupiter-db:${version}"
```

For jOOQ support, use this dependency instead. The jOOQ library adds support for injecting
a `org.jooq.DSLContext` instance instead of a JDBC connection.
```groovy
testCompile "com.mostlycertain:jupiter-db-jooq:${version}"
```

Add the `@DatabaseTest` annotation to the test fixture. This loads the extension and gives the
option to set database connection information. The database connection information is optional
and can be set or overridden with system properties.

Add parameters to the test or the test class `@BeforeEach` to inject the database connection.

### Connection Configuration

The database connection settings (url, user, password) can be completely or partially set using
the `@DatabaseTest` annotation. Those values can be set or overridden at runtime using
system properties.

System properties:
- `jupiterdb.database.url` - JDBC connection URL
- `jupiterdb.database.user` - Database username
- `jupiterdb.database.password` - Database password

### Initializing Tests

The extension has annotations that can be used to specify SQL statements to execute before each
test runs. This is useful for setting up test data.

#### @InitializeSql

Execute SQL before each test is invoked.

Inline SQL can specified with the `value` parameter. Multiple statements can be included in the
SQL string, separated with semicolons.

SQL statements can be loaded from a resource file by setting the `resource` parameter to a resource
file name. The resource file name is relative to the test class. The file can contain multiple
statements, separated by semicolons.

This annotation can be applied to the test class or individual test methods.
The SQL provided is executed on the database connection before each test is run. The SQL
attached to the test class is executed before the SQL attached to the test method.

#### @FinalizeSql

Execute SQL before each test is invoked, but after all `@InitializeSql` has been executed.

For example, if you are using postgres, this annotation can be used to switch to a more
restricted database role after the database has been initialized.

The options and usage are the same as `@InitializeSql`.

### Examples

#### Inject the Connection into the Test
```java
import java.sql.Connection;
import com.mostlycertain.jupiter.db.DatabaseTest;

@DatabaseTest(
        url = "jdbc:postgresql://host/db",
        user = "postgres",
        password = "password"
)
class FooTest {
    @Test
    void test(Connection connection) {
        // Do some database work
    }
}
```


#### Inject the Connection into BeforeEach
```java
import java.sql.Connection;
import com.mostlycertain.jupiter.db.DatabaseTest;

@DatabaseTest(
        url = "jdbc:postgresql://host/db",
        user = "postgres",
        password = "password"
)
class FooTest {
    Connection connection;
    
    @BeforeEach
    void beforeEach(Connection connection) {
        this.connection = connection;
    }

    @Test
    void test() {
        // Do some database work
    }
}
```

#### Execute SQL

```java
@DatabaseTest
@InitializeSql("DELETE FROM foo; DELETE from bar")
@FinalizeSql("SET ROLE service_user")
class FooTest {
    Connection connection;
    
    @BeforeEach
    void beforeEach(Connection connection) {
        this.connection = connection;
    }

    @Test
    @InitializeSql("DELETE FROM biz")
    @InitializeSql(resource = "test_init.sql")
    void test() {
        // Do some database work
    }
}
```
