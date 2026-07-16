package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.Getter;
import lombok.Setter;


@SpringBootApplication
@Controller
public class WebShop {
	
	public static void main(String[] args) {                       // メインでファィル名を設定している
		OutputFile.location = "C:\\Users\\ktc\\useridinfo.txt";
		SpringApplication.run(WebShop.class, args);
	}
	
	@RequestMapping("/webshop")                        // URL webshopをクライアントとして要求する
	public String Start() {
			System.out.println("User Login");         // メッセージをコンソールに出力（処理の確認用）
			return "loginorg.html";                      // Web画面には出力されない
		}	

	@PostMapping("UserLogin")
	public String loginview(Model model,@RequestParam String userid, @RequestParam String password)  {
	    String rtnhtml  = "passlogin";		
		if (userid.equals("LOgIN0000"))  {                  // LOgIN0000はユーザ名が重ならないように設定
		   //System.out.println("Login from Regist");
		   rtnhtml = "login.html";                          // ログイン画面に戻す
		   return rtnhtml;  }
	    
	    if (userid.equals("")) {                   // ユーザ(ID,パスワード,氏名)の登録
	    	   System.out.println("User ID is empty  re-Regist");
	    	   rtnhtml ="registid";  }
	    else {
	    if (!PasswordCheck(userid,password))  {    // ユーザIDとパスワードをチェックする
	  	   System.out.println("Password check : deny  re-Login");
	    	   rtnhtml ="relogin";   }                 // IDあるいはパスワードの誤りによる再度ログイン
	    else  {                                    // 指定されたユーザIDが見つかり、顧客名を得る
	    	   String name = getName(userid);          
	    	   model.addAttribute("name",name);        // 顧客名をhtmlに返す 
	    	   rtnhtml = "passlogin";                  // Login成功
	          }
	    }         
		return rtnhtml;                            // htmlを呼び出す
	}

	
	
	// Check User Id and password   Must be replaced it to Database  
	public boolean PasswordCheck(String id,String pass)  {	        // 入力されたパスワードのチェック
		if (readFile(OutputFile.location, id) == 0) { 
	       if (pass.equals(Info.customerPassword)) {
	    	      return true;   }                                        // 一致するとtrueを返す
		}
		return false;
	}
	
	// Check an existing file or not
	public boolean CheckExsitingId(String id) {		                // 同じユーザIDがあるかどうかチェックする
	    if (readFile(OutputFile.location, id) == -1) {             // 同じユーザIDが登録されていないと-1が返ってくる
		   return true;   }                                          
	    return false;
	}		

	// Get Customer name from user ID     Must be replaced it to Database 
	public String getName(String id) {                               // 指定されたユーザーIDの氏名を返す
		if (readFile(OutputFile.location, id) == 0) {              // ユーザーIDが存在していれば、0 が返ってくる
	       return Info.customerName;  }
		return "Not found user name"; 
	}
	
	  private static boolean checkBeforeReadfile(File file)  {        // 指定されたファィルが存在するかどうかのチェック
	    if (file.exists())  {
	      if (file.isFile() && file.canRead())  {
	        return true;  }
	    }
	    return false;
	  }

		  
	  // Read text(User Id,Password,Name) from the file
	  public static  int readFile(String textfilename,String id)  {                //  指定されたファィルから顧客情報を読み込む
		int rtncode = 0;  
	    try  {
	      File text = new File(textfilename);
	      if (checkBeforeReadfile(text))  {
	        BufferedReader brt = new BufferedReader(new FileReader(text));
	        String str;
	        while((str = brt.readLine()) != null)  {
	        	  int i = 0;
	        	  String[] personalData= new String[100]; 
	        	  String[] pData = str.split(",");                     // 読み込むテキストは,で区切られている
	          for (String person :pData)  {
	            personalData[i++] = person;
	          }
	     
	          if (personalData[0].equals(id)) {                    // 指定されたIDをファィル内にあるかどうかチェック
	        	    Info.customerID = personalData[0];                // ユザーID
	        	    Info.customerPassword = personalData[1];         // パスワード
	        	    Info.customerName = personalData[2];              // 顧客名 
	        	    rtncode = 0;                                       // 指定されたIDと同じIDがファィル内で見つかれば、検索終了 
	        	    break;
	          } else  {
	        	    rtncode = -1;  }                                    // 指定されたIDが見つからない  
	        }
	          brt.close();    }                                     //  ファィルのクローズ
	      else  {
	           System.out.println("Failed to open the file"); 
	           rtncode = -2;  }                                     // ファィルのオープンに失敗
	    }
	    
	    catch(FileNotFoundException e)  {
	         System.out.println(e);
			 rtncode = -11;  }                            // Failed  No file   Exception Error} 
	    catch(IOException e)  {
	         System.out.println(e);
			 rtncode = -12;  }                           // Failed  I/O Error  Exception Error}
		
		return rtncode;  
	  }

	  @Setter
	  @Getter
	  public class Info  {

		 static String customerID;
		 static String customerPassword;
		 static String customerName;
	  }
	  
	  public class OutputFile  {

		 static String location; 
	  }

	  
}
	

