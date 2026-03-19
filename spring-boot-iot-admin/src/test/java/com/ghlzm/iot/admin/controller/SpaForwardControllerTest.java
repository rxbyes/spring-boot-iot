package com.ghlzm.iot.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaForwardControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new SpaForwardController(), new IndexHtmlStubController())
            .build();

    @Test
    void shouldForwardRootAndFrontendRoutesToIndexHtml() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/products").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/risk-disposal/workbench/detail").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/message/http/report").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/message/mqtt/down/publish").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldNotCaptureApiLikeRoutes() throws Exception {
        mockMvc.perform(get("/api").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/message/http/report").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/assets/app.js").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
    }

    @Controller
    static class IndexHtmlStubController {

        @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
        @ResponseBody
        String index() {
            return "index";
        }
    }
}
