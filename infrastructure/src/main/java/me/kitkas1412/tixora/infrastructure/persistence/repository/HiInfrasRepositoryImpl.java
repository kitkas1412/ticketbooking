package me.kitkas1412.tixora.infrastructure.persistence.repository;

import me.kitkas1412.tixora.domain.repository.HiDomainRepository;
import org.springframework.stereotype.Service;

@Service
public class HiInfrasRepositoryImpl implements HiDomainRepository {
    @Override
    public String sayHi(String who) {
        return "Hi Infrastructure";
    }
}
