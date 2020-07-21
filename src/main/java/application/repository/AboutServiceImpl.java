package application.repository;

import org.springframework.stereotype.Service;

import application.model.About;

@Service
public class AboutServiceImpl implements AboutService{

	@Override
	public About getInfo() {
		// TODO Auto-generated method stub
		return new About("Customer Service", "Storefront", "Manages all customer data");
	}
	
}
