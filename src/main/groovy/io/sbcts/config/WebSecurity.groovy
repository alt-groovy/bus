package io.sbcts.config

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
@Profile('!test')
public class WebSecurity extends WebSecurityConfigurerAdapter {

    @Value('${ad.url}')
    private String AD_HOST;

    @Value('${ad.port}')
    private int AD_PORT;

    @Value('${ad.user.searchbase}')
    private String AD_USER_BASE;

    @Value('${ad.group.searchbase}')
    private String AD_GROUP_BASE;

    @Value('${ad.manager.user}')
    private String AD_MANAGER_USER;

    @Value('${ad.manager.password}')
    private String AD_MANAGER_PASSWORD;

    @Value('${ad.group}')
    private String AD_GROUP;

    @Value('${ad.user.searchfield}')
    private String AD_SEARCH_FIELD;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/logout", "/login").permitAll()
                .antMatchers("/", "/api/**","/actuator/**")
                .permitAll()
//                .hasRole(AD_GROUP)
                .and()
                .httpBasic().and()
                .formLogin().permitAll().and()
                .logout()
                .permitAll();

    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .ldapAuthentication()

                .userSearchFilter('('+AD_SEARCH_FIELD+'={0})')
                .userSearchBase(AD_USER_BASE)
                .groupSearchBase(AD_GROUP_BASE)
                .groupSearchFilter('member={0}')

                .contextSource()
                .url(AD_HOST)
                .port(AD_PORT)
                .managerDn(AD_MANAGER_USER)
                .managerPassword(AD_MANAGER_PASSWORD);
    }

}
