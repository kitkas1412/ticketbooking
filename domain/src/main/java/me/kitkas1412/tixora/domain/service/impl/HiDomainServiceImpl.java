package me.kitkas1412.tixora.domain.service.impl;

import me.kitkas1412.tixora.domain.repository.HiDomainRepository;
import me.kitkas1412.tixora.domain.service.HiDomainService;
import org.springframework.stereotype.Service;

@Service
public class HiDomainServiceImpl implements HiDomainService {

    private final HiDomainRepository hiDomainRepository;

    public HiDomainServiceImpl(HiDomainRepository hiDomainRepository) {
        this.hiDomainRepository = hiDomainRepository;
    }

    @Override
    public String sayHi(String who) {
        return hiDomainRepository.sayHi(who);
    }
}
