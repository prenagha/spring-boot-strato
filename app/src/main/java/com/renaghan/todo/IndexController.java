package com.renaghan.todo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

  public IndexController() {}

  @GetMapping
  @RequestMapping("/")
  public String getIndex() {
    return "index";
  }
}
