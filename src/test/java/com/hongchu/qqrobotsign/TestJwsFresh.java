package com.hongchu.qqrobotsign;

import com.hongchu.qqrobotsign.service.IUserService;
import com.hongchu.qqrobotsign.service.SignService;
//import com.hongchu.qqrobotsign.task.RedisDelaySignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestJwsFresh {
//    @Autowired private RedisDelaySignService delaySignService;
    @Autowired private IUserService userService;
    @Test
    public void jwsFresh(){
        userService.refreshJws("202307070211");
    }

    @Test
    public void jwsFreshs(){
//        delaySignService.refreshJwsTrigger();
    }
}
