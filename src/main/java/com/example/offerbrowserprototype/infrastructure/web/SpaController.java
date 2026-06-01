package com.example.offerbrowserprototype.infrastructure.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

// Forwards React Router paths to index.html. Every segment must have no dot so
// static assets like /assets/main.css are never intercepted here.
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/{p1:[^\\.]+}",
        "/{p1:[^\\.]+}/{p2:[^\\.]+}",
        "/{p1:[^\\.]+}/{p2:[^\\.]+}/{p3:[^\\.]+}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
