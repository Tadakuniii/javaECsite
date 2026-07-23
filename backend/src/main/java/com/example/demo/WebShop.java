package com.example.demo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class WebShop {
	
	private final CustomerRepository customerRepository;

	public WebShop(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@RequestMapping("/api/webshop")
	public Map<String, Object> start() {
		System.out.println("User Login request received");
		Map<String, Object> response = new HashMap<>();
		response.put("status", "INITIAL");
		response.put("message", "Please login");
		return response;
	}	

	@PostMapping("/api/login")
	public Map<String, Object> loginView(@RequestParam String userid, @RequestParam String password) {
		Map<String, Object> response = new HashMap<>();
		
		if ("LOgIN0000".equals(userid)) {
			response.put("status", "DEBUG_LOGIN");
			response.put("message", "ログインしてください");
			return response;
		}
	    
		if (userid.isEmpty()) {
			System.out.println("User ID is empty, redirecting to registration");
			response.put("status", "EMPTY_ID");
			response.put("message", "User ID is empty");
			return response;
		}

		if (!passwordCheck(userid, password)) {
			System.out.println("Password check failed for user: " + userid);
			response.put("status", "RELOGIN");
			response.put("message", "ユーザIDが見つからないか、パスワードが間違っています");
			return response;
		}

		String name = getCustomerName(userid);          
		response.put("status", "SUCCESS");
		response.put("username", name);
		return response;
	}

	@PostMapping({"/api/RegistUserID", "/api/registid", "/api/register"})
	public Map<String, Object> registerUser(@RequestParam String userid,
	                                        @RequestParam String password1,
	                                        @RequestParam String password2,
	                                        @RequestParam(required = false, defaultValue = "") String username) {
		Map<String, Object> response = new HashMap<>();

		if (!password1.equals(password2)) {
			response.put("status", "PASSWORD_MISMATCH");
			response.put("message", "異なるパスワードが入力されました");
			return response;
		}
		if (isUserIdRegistered(userid)) {
			response.put("status", "EXISTING_ID");
			response.put("message", "入力されたユーザIDは登録済です");
			return response;
		}
		
		Customer customer = new Customer(userid, password1, username.isEmpty() ? "新規ユーザー" : username);
		customerRepository.save(customer);
		
		response.put("status", "SUCCESS");
		response.put("message", "ユーザIDが登録されました");
		return response;
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
