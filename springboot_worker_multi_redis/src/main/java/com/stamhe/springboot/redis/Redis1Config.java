package com.stamhe.springboot.redis;

import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


@Configuration
public class Redis1Config {
	@Value("${spring.redis1.host}")
    private String host;
	
	@Value("${spring.redis1.port}")
    private Integer port;
	
	@Value("${spring.redis1.password}")
    private String password;
	
	@Value("${spring.redis1.database}")
    private Integer database;

	@Value("${spring.redisconfig.pool.max-active}")
    private Integer maxActive;
	
    @Value("${spring.redisconfig.pool.max-idle}")
    private Integer maxIdle;
    
    @Value("${spring.redisconfig.pool.max-wait}")
    private Long maxWait;
    
    @Value("${spring.redisconfig.pool.min-idle}")
    private Integer minIdle;
    

    /*
     * 使用springdboot操作Redis时，发现key值出现 \xac\xed\x00\x05t\x00\tb，但不影响程序读写，
     * 查询资料发现redisTemplate 默认的序列化方式为 jdkSerializeable, StringRedisTemplate的默认序列化方式为StringRedisSerializer
     * 可以通过手动配置, 将redisTemplate的序列化方式进行更改
     */
    @Bean(name="redis1Template")
    public RedisTemplate<String, Object> redis1Template(@Qualifier("lettuce1Factory")LettuceConnectionFactory lettuce1Factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuce1Factory);
        
        ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(om);
		
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		
		redisTemplate.setValueSerializer(genericJackson2JsonRedisSerializer);
		redisTemplate.setHashValueSerializer(genericJackson2JsonRedisSerializer);
		
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean(name="lettuce1Factory")
    @Primary
    public LettuceConnectionFactory lettuce1Factory(RedisStandaloneConfiguration redis1RedisConfig,
            GenericObjectPoolConfig redis1PoolConfig) {
        LettuceClientConfiguration clientConfig =
                LettucePoolingClientConfiguration.builder().commandTimeout(Duration.ofMillis(100))
                        .poolConfig(redis1PoolConfig).build();
        return new LettuceConnectionFactory(redis1RedisConfig, clientConfig);
    }

    
    @Bean(name="redis1PoolConfig")
    public GenericObjectPoolConfig poolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWait);
        return config;
    }

    @Bean(name="redis1RedisConfig")
    public RedisStandaloneConfiguration redisConfig() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPassword(RedisPassword.of(password));
        config.setPort(port);
        config.setDatabase(database);
        return config;
    }
}
