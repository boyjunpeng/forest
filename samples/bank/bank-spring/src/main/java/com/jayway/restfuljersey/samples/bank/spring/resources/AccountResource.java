package com.jayway.restfuljersey.samples.bank.spring.resources;

import static com.jayway.forest.grove.RoleManager.context;

import com.jayway.jersey.rest.resource.Resource;
import com.jayway.jersey.rest.roles.DescribedResource;
import com.jayway.restfuljersey.samples.bank.dto.TransferToDTO;
import com.jayway.restfuljersey.samples.bank.model.Account;
import com.jayway.restfuljersey.samples.bank.model.AccountManager;
import com.jayway.restfuljersey.samples.bank.model.Depositable;
import com.jayway.restfuljersey.samples.bank.model.Withdrawable;
import com.jayway.restfuljersey.samples.bank.repository.AccountRepository;
import com.jayway.restfuljersey.samples.bank.spring.ResourceWithAccount;
import com.jayway.restfuljersey.samples.bank.spring.constraints.DepositAllowed;
import com.jayway.restfuljersey.samples.bank.spring.constraints.HasCredit;
import com.jayway.restfuljersey.samples.bank.spring.constraints.IsWithdrawable;

public class AccountResource implements Resource, DescribedResource, ResourceWithAccount {

    private final Account account;

	public AccountResource(Account account) {
		this.account = account;
	}

	public void allowexceeddepositlimit( Boolean allow ) {
    	account.setAllowExceedBalanceLimit(allow);
    }

    @DepositAllowed
    public void deposit( Integer amount ) {
        context(AccountManager.class).deposit((Depositable) account, amount);
    }

    @HasCredit
    @IsWithdrawable
    public void withdraw( Integer amount ) {
        context(AccountManager.class).withdraw((Withdrawable) account, amount);
    }

    @HasCredit
    @IsWithdrawable
    public void transfer( TransferToDTO transfer ) {
        Depositable depositable = context(AccountRepository.class).findWithRole(transfer.getDestinationAccount(), Depositable.class);
        Withdrawable withdrawable = (Withdrawable) account;

        context(AccountManager.class).transfer(withdrawable, depositable, transfer.getAmount() );
    }


    @Override
    public Object description() {
        return String.format( Account.HTML_DESCRIPTION, account.getAccountNumber(), account.getBalance(), account.isAllowExceedBalanceLimit() );
    }

	@Override
	public Account getAccount() {
		return account;
	}
}