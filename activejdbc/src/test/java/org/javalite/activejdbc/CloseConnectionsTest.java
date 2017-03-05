package org.javalite.activejdbc;

import static org.javalite.activejdbc.test.JdbcProperties.driver;
import static org.javalite.activejdbc.test.JdbcProperties.password;
import static org.javalite.activejdbc.test.JdbcProperties.url;
import static org.javalite.activejdbc.test.JdbcProperties.user;

import org.javalite.test.jspec.JSpecSupport;
import org.junit.Test;

/**
 * @author Igor Polevoy
 */
public class CloseConnectionsTest implements JSpecSupport {

    @Test
    public void shouldCloseAllConnections(){

    	
        new DB("conection1").open(driver(), url(), user(), password());
        new DB("conection2").open(driver(), url(), user(), password());

        a(DB.getCurrrentConnectionNames().size()).shouldBeEqual(2);

        DB.closeAllConnections();

        a(DB.getCurrrentConnectionNames().size()).shouldBeEqual(0);
    }

    @Test
    public void shouldTryWithResources(){
        try(DB db = new DB()){
            db.open();
            a(DB.getCurrrentConnectionNames().size()).shouldBeEqual(1);
        }
        a(DB.getCurrrentConnectionNames().size()).shouldBeEqual(0);
    }
}
