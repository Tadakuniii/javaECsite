package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebShop {
	
	private final CustomerRepository customerRepository;

	public WebShop(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@RequestMapping("/webshop")
	public String start() {
		System.out.println("User Login request received");
		return "loginorg.html";
	}	

	@PostMapping("UserLogin")
	public String loginView(Model model, @RequestParam String userid, @RequestParam String password) {
		if ("LOgIN0000".equals(userid)) {
			return "login.html";
		}
	    
		if (userid.isEmpty()) {
			System.out.println("User ID is empty, redirecting to registration");
			return "registid";
		}

		if (!passwordCheck(userid, password)) {
			System.out.println("Password check failed for user: " + userid);
			return "relogin";
		}

		String name = getCustomerName(userid);          
		model.addAttribute("name", name);
		return "passlogin";
	}

	@PostMapping({"/RegistUserID", "/registid"})
	public String registerUser(Model model,
	                           @RequestParam String userid,
	                           @RequestParam String password1,
	                           @RequestParam String password2,
	                           @RequestParam(required = false, defaultValue = "") String username) {
		if (!password1.equals(password2)) {
			return "registpassdeny";
		}
		if (isUserIdRegistered(userid)) {
			return "registidexisting";
		}
		
		Customer customer = new Customer(userid, password1, username.isEmpty() ? "新規ユーザー" : username);
		customerRepository.save(customer);
		return "registaccept";
	}

	private boolean passwordCheck(String id, String pass) {
		return customerRepository.findById(id)
				.map(c -> c.getCustomerPassword().equals(pass))
				.orElse(false);
	}
	
	private boolean isUserIdRegistered(String id) {
		return customerRepository.existsById(id);
	}		

	private String getCustomerName(String id) {
		return customerRepository.findById(id)
				.map(Customer::getCustomerName)
				.orElse("Not found user name");
	}
}
