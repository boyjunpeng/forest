package com.jayway.forest.samples.bank.jersey.resources;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.jayway.forest.core.RoleManager;
import com.jayway.forest.core.Setup;
import com.jayway.forest.hypermedia.HyperMediaResponse;
import com.jayway.forest.hypermedia.Link;
import com.jayway.forest.roles.Resource;
import com.jayway.forest.samples.bank.jersey.resources.accounts.AccountsResource;
import com.jayway.forest.samples.bank.model.AccountManager;
import com.jayway.forest.samples.bank.repository.AccountRepository;
import com.sun.jersey.spi.resource.Singleton;

@Path("")
@Singleton
public class RootResource implements Resource {
	
	private final AccountManager accountManager = new AccountManager();
	private final AccountRepository accountRepository = new AccountRepository();
	
	public RootResource() {
        Setup.setup();
		accountRepository.initializeDummyAccounts(accountManager);
	}

	@GET
	public HyperMediaResponse<String> root() {
		HyperMediaResponse<String> response = new HyperMediaResponse<String>(this.getClass().getName(), "qweqwe", String.class);
		response.addLink(new Link("simpleEcho", "GET", "simpleEcho", "documentation"));
		response.addLink(new Link("command", "POST", "command", "N/A"));
		return response;
	}

	@Path("simpleEcho")
	@GET
	public String simpleEcho(@QueryParam("param") String value) {
		if (value == null) {
			return "QWEQWEQWE";
		}
		return value;
	}

	@Path("simplePost")
	@POST
	public void simplePost(@FormParam("param") String value) {
	}

	@Path("simplePost")
	@GET
	public String simplePost() {
		return "QWE";
	}

	@Path("command")
	@POST
	public void command(@FormParam("param") String value) {
		System.out.println(value);
	}

	@Path("accounts/")
    public AccountsResource accounts() {
		RoleManager.addRole(AccountRepository.class, accountRepository);
		RoleManager.addRole(AccountManager.class, accountManager);
        return new AccountsResource();
    }
}