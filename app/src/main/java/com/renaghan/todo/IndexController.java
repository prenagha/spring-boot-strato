package com.renaghan.todo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class IndexController {

  public IndexController() {}

  @RequestMapping(method = RequestMethod.GET, path = "/")
  public String getIndex() {
    return "index";
  }
}
