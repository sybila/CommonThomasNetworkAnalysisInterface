package ctnai.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BehaviourMapController
{
    @RequestMapping(value = "/BehaviourMapInterface", method = RequestMethod.GET)
    public String getBehaviourMapInterface(ModelMap model, @RequestParam("file") Long id)
    {
        model.addAttribute("file", id);
        
        return "behaviourMapInterface";
    }
}
