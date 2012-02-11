package com.jayway.forest.samples.bank.spring.resources;

import java.util.List;

import com.jayway.forest.samples.bank.dto.AccountTransformer;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.forest.legacy.roles.IdDiscoverableResource;
import com.jayway.forest.legacy.roles.Linkable;
import com.jayway.forest.legacy.roles.Resource;
import com.jayway.forest.legacy.servlet.ResponseHandler;
import com.jayway.forest.samples.bank.model.Account;
import com.jayway.forest.samples.bank.repository.AccountRepository;

public class AccountsResource implements Resource, IdDiscoverableResource {
	
	@Autowired
	private AccountRepository accountRepository;

    @Override
    public Resource id(String id) {
        Account account = accountRepository.findById(id);
        return new AccountResource(account);
    }

    @Override
    public List<Linkable> discover() {
        return ResponseHandler.transform( accountRepository.all(), AccountTransformer.INSTANCE );
    }
}
