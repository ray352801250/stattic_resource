package com.dodoca;


import com.dodoca.service.TestService;
import com.dodoca.utils.RedisTool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class StaticResourceServerApplicationTests {

    @Autowired
    TestService testService;

    @Test
    public void test1() {
        testService.test();
    }

}
