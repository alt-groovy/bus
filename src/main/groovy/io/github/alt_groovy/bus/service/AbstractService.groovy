package io.github.alt_groovy.bus.service


import groovy.sql.Sql

import javax.sql.DataSource

abstract class  AbstractService {


    DataSource getContextDataSource(String context){

        DataSource ds
        switch (context) {
            case 'dev' : ds = devDS; break;
            case 'test' : ds = testDS; break;
            case 'preprod' : ds = preprodDS; break;
            case 'prod' : ds = prodDS; break;
        }
        return ds;
    }

    Sql getSql (String context){
        return Sql.newInstance(getContextDataSource(context))
    }

    int ceiling (int number, int ceiling){
        return  (number <= ceiling ? number : ceiling);
    }

    protected Map stripNulls (map) {
        def noNulls = [:]
        map.each { key, value ->
            if (value != null && !'null'.equals(value.toString())) noNulls[key] = value
        }
        return noNulls
    }


}
