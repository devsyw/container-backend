package kr.osci.container.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@Slf4j
public class ServiceController {

    @Value("${kubernetes.domain:localhost}")
    private String domain;

    @Value("${kubernetes.ingress.port:30080}")
    private int ingressPort;

    @Value("${platform.jupyterhub.enabled:false}")
    private boolean jupyterhubEnabled;

    @Value("${platform.jupyterhub.name:JupyterHub}")
    private String jupyterhubName;

    @Value("${platform.jupyterhub.description:멀티유저 Jupyter 환경}")
    private String jupyterhubDescription;

    @Value("${platform.jupyterhub.icon:🪐}")
    private String jupyterhubIcon;

    @Value("${platform.jupyterhub.subdomain:jupyterhub}")
    private String jupyterhubSubdomain;

    @GetMapping
    public List<Map<String, Object>> getServices() {
        List<Map<String, Object>> services = new ArrayList<>();

        if (jupyterhubEnabled) {
            Map<String, Object> jupyterhub = new HashMap<>();
            jupyterhub.put("id", "jupyterhub");
            jupyterhub.put("name", jupyterhubName);
            jupyterhub.put("description", jupyterhubDescription);
            jupyterhub.put("icon", jupyterhubIcon);
            jupyterhub.put("url", String.format("http://%s.%s:%d", jupyterhubSubdomain, domain, ingressPort));
            services.add(jupyterhub);
        }

        return services;
    }
}