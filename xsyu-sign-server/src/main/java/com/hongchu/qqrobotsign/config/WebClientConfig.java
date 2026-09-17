package com.hongchu.qqrobotsign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * 通用的WebClient，用于桌面端HTTP请求
     * User-Agent: 桌面浏览器
     */
    @Bean("webClient")
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT,
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br")
                .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
                .build();
    }

    /**
     * 移动端WebClient，用于模拟手机访问
     * User-Agent: 移动端浏览器
     */
    @Bean("mobileWebClient")
    public WebClient mobileWebClient() {
        return WebClient.builder()
                .codecs(configurer -> {
                    // 启用所有编解码器，包括压缩解压缩
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
                })
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT,
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9")
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "identity") // 明确表示不接收压缩数据
                .defaultHeader("Sec-Fetch-Dest", "document")
                .defaultHeader("Sec-Fetch-Mode", "navigate")
                .defaultHeader("Sec-Fetch-Site", "none")
                .defaultHeader("Upgrade-Insecure-Requests", "1")
                .build();
    }

    /**
     * 专门用于API请求的WebClient
     * 适用于REST API调用，默认使用JSON格式
     */
    @Bean("apiWebClient")
    public WebClient apiWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .build();
    }

    /**
     * 高配版WebClient，适用于大文件下载或复杂请求
     * 包含连接池和更大的内存限制
     */
    @Bean("advancedWebClient")
    public WebClient advancedWebClient() {
        // 配置更大的内存限制
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        // 配置HttpClient超时设置
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
                .followRedirect(true);

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return WebClient.builder()
                .clientConnector(connector)
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .build();
    }
}