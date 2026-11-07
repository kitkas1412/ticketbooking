package me.kitkas1412.tixora.application.service.event.impl;

import me.kitkas1412.tixora.application.service.event.EventAppService;
import me.kitkas1412.tixora.domain.service.HiDomainService;
import org.springframework.stereotype.Service;

@Service
public class EventAppServiceImpl implements EventAppService {

    private final HiDomainService hiDomainService;

    public EventAppServiceImpl(HiDomainService hiDomainService) {
        this.hiDomainService = hiDomainService;
    }

    @Override
    public String sayHi(String who) {
        return hiDomainService.sayHi(who);
    }
}
