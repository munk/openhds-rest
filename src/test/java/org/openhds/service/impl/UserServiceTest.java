package org.openhds.service.impl;

import org.openhds.security.model.User;
import org.openhds.service.UuidServiceTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Wolfe on 7/13/2015.
 */
public class UserServiceTest extends UuidServiceTest<User, UserService> {
    @Override
    protected User makeInvalidEntity() {
        return new User();
    }

    @Override
    @Autowired
    protected void initialize(UserService service) {
        this.service = service;
    }
}
