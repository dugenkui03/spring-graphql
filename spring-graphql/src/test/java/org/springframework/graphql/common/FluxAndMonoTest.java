package org.springframework.graphql.common;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

public class FluxAndMonoTest {

    class DemoClass {
        public DemoClass() {
            System.out.println("init DemoClass instance");
        }
    }

    @Test
    public void monoJustTest() {
        // kp new DemoClass() 作为实参提前创建
        Mono<DemoClass> demoClassMono = Mono.just(new DemoClass());
        System.out.println("before invoke demoClassMono.block");

        // kp 获取的始终是预先创建的 DemoClass对象
        for (int i = 0; i < 10; i++) {
            demoClassMono.block();
        }
    }

    @Test
    public void monoDeferTest() {
        Mono<DemoClass> deferDemoClass = Mono.defer(() -> Mono.just(new DemoClass()));
        System.out.println("before invoke demoClassMono.block");

        // kp 每次调用 deferDemoClass.block() 都会重新创建 DemoClass 对象
        for (int i = 0; i < 10; i++) {
            deferDemoClass.block();
        }
    }
}
