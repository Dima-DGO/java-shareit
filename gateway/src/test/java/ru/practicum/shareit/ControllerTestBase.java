package ru.practicum.shareit;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.practicum.shareit.exception.ErrorHandler;

public abstract class ControllerTestBase {
    protected MockMvc mvc;

    protected void setUpMvc(Object controller) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ErrorHandler())
                .setValidator(validator)
                .build();
    }
}
