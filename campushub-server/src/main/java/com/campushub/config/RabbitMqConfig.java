package com.campushub.config;

import com.campushub.constant.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ拓扑声明：交换机、队列、绑定全部在此显式声明（durable），
 * 应用启动时由RabbitAdmin自动创建，避免依赖控制台手工建拓扑。
 */
@Slf4j
@Configuration
public class RabbitMqConfig {

    /**
     * 显式声明RabbitAdmin（返回具体类型）。
     * Spring Boot 3.5 自动配置的amqpAdmin工厂方法返回的是接口类型AmqpAdmin，
     * 而用户Bean解析顺序先于自动配置Bean，按具体类型RabbitAdmin注入时
     * Spring只能拿到接口类型的预判信息，判定找不到Bean。自己声明则类型确定、顺序无关。
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // ==================== 死信拓扑 ====================

    /**
     * 死信交换机：fanout类型，消费失败且超过重试上限的消息
     * 无论原路由键是什么，统一落入死信队列。
     */
    @Bean
    public FanoutExchange deadExchange() {
        return ExchangeBuilder.fanoutExchange(MqConstant.DEAD_EXCHANGE).durable(true).build();
    }

    /**
     * 死信队列。
     */
    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable(MqConstant.DEAD_QUEUE).build();
    }

    /**
     * 死信队列绑定死信交换机。
     */
    @Bean
    public Binding deadQueueBinding() {
        return BindingBuilder.bind(deadQueue()).to(deadExchange());
    }

    // ==================== 违约事件拓扑 ====================

    /**
     * 预约域交换机：direct类型，按路由键精确路由。
     */
    @Bean
    public DirectExchange bookingExchange() {
        return ExchangeBuilder.directExchange(MqConstant.BOOKING_EXCHANGE).durable(true).build();
    }

    /**
     * 信用分扣减队列：声明死信交换机，消费失败超限的消息进入死信队列。
     */
    @Bean
    public Queue creditBreachQueue() {
        return QueueBuilder.durable(MqConstant.CREDIT_BREACH_QUEUE)
                .deadLetterExchange(MqConstant.DEAD_EXCHANGE)
                .build();
    }

    /**
     * 站内通知队列：同样挂死信配置。
     */
    @Bean
    public Queue notifyBreachQueue() {
        return QueueBuilder.durable(MqConstant.NOTIFY_BREACH_QUEUE)
                .deadLetterExchange(MqConstant.DEAD_EXCHANGE)
                .build();
    }

    /**
     * 信用分扣减队列绑定预约交换机。
     */
    @Bean
    public Binding creditBreachQueueBinding() {
        return BindingBuilder.bind(creditBreachQueue())
                .to(bookingExchange())
                .with(MqConstant.BOOKING_BREACH_ROUTING_KEY);
    }

    /**
     * 站内通知队列绑定预约交换机。
     */
    @Bean
    public Binding notifyBreachQueueBinding() {
        return BindingBuilder.bind(notifyBreachQueue())
                .to(bookingExchange())
                .with(MqConstant.BOOKING_BREACH_ROUTING_KEY);
    }

    // ==================== 延迟消息拓扑（Phase 4） ====================

    /**
     * 延迟交换机：x-delayed-message 插件类型，不是AMQP原生类型，
     * 必须用CustomExchange声明，并通过 x-delayed-type 参数告诉插件
     * 到点后按direct规则路由。
     * 延迟期间消息保存在插件内部（broker的mnesia表），不进队列——
     * 控制台Queues页看不到，只有Exchange详情页能看到该交换机上的消息计数。
     */
    @Bean
    public CustomExchange bookingDelayExchange() {
        return new CustomExchange(
                MqConstant.BOOKING_DELAY_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                Map.of("x-delayed-type", "direct")
        );
    }

    /**
     * 到点核销检查队列：同样挂死信交换机，消费失败超限进死信兜底。
     */
    @Bean
    public Queue bookingDelayCheckQueue() {
        return QueueBuilder.durable(MqConstant.BOOKING_DELAY_CHECK_QUEUE)
                .deadLetterExchange(MqConstant.DEAD_EXCHANGE)
                .build();
    }

    /**
     * 检查队列绑定延迟交换机。CustomExchange没有类型化的路由配置器，
     * 用 .with(routingKey).noargs() 完成通用绑定。
     */
    @Bean
    public Binding bookingDelayCheckQueueBinding() {
        return BindingBuilder.bind(bookingDelayCheckQueue())
                .to(bookingDelayExchange())
                .with(MqConstant.BOOKING_DELAY_CHECK_ROUTING_KEY)
                .noargs();
    }

    /**
     * 启动拓扑声明：RabbitAdmin默认行为是"首次建立连接时才统一声明全部Bean"，
     * 但当前应用既无生产者也无消费者，连接永远不会建立，拓扑永远不会被创建。
     * 此处在启动阶段手动触发initialize()：一方面确保拓扑随应用启动即创建；
     * 更重要的是实现fail-fast——连接、vhost、权限配置有误会直接启动报错，
     * 而不是被静默隐藏直到第一次真正收发消息时才暴露。
     */
    @Bean
    public ApplicationRunner rabbitTopologyInitializer(RabbitAdmin rabbitAdmin) {
        return args -> {
            rabbitAdmin.initialize();
            log.info("[RabbitMQ] 拓扑声明完成：2个交换机、3个队列");
        };
    }
}
