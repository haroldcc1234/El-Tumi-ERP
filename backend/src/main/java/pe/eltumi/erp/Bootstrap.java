package pe.eltumi.erp;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
import java.time.*;
import java.math.*;
import static pe.eltumi.erp.Store.*;
@Component
public class Bootstrap implements ApplicationRunner {
 final Store db;final BCryptPasswordEncoder passwords;final Inventory inventory;final Ledger ledger;
 @Value("${app.demo}")boolean demo;@Value("${app.admin-email}")String email;@Value("${app.admin-password}")String password;
 public Bootstrap(Store d,BCryptPasswordEncoder p,Inventory i,Ledger l){db=d;passwords=p;inventory=i;ledger=l;}
 @Transactional public void run(ApplicationArguments args){if(db.count("SELECT COUNT(*) FROM company")>0)return;if(!demo&&password.equals("ElTumi2026!"))throw new IllegalStateException("Define ADMIN_PASSWORD antes de iniciar sin datos demo");require(password.length()>=10&&password.length()<=72,"ADMIN_PASSWORD requiere de 10 a 72 caracteres");
 db.insert("company",map("name","El Tumi — empresa de demostración","trade_name","El Tumi","ruc","20000000001","address","Av. Carretera Principal s/n, Ingenio","ubigeo","120109","department","Junín","province","Huancayo","district","Ingenio","phone","","id_threshold",700,"digital_threshold",2000));
 db.insert("users",map("name","Administrador El Tumi","email",email.toLowerCase(),"password_hash",passwords.encode(password),"role","ADMIN","status","ACTIVO"));
 for(String role:List.of("CONTADOR","FINANZAS","ALMACEN","RRHH","CAJA","SALON","COCINA","MARKETING"))for(String module:List.of("accounting","finance","logistics","hr","sales","admin")){boolean read=switch(role){case "CONTADOR"->List.of("accounting","finance","logistics","sales").contains(module);case "FINANZAS"->List.of("finance","accounting").contains(module);case "ALMACEN"->module.equals("logistics");case "RRHH"->module.equals("hr");default->module.equals("sales");};boolean write=read&&!role.equals("COCINA")&&!role.equals("MARKETING");boolean approve=write&&!role.equals("SALON");if(role.equals("CONTADOR")&&!module.equals("accounting")){write=false;approve=false;}db.insert("permissions",map("role",role,"module",module,"can_read",read,"can_write",write,"can_approve",approve,"can_export",read&&!role.equals("COCINA")));}
 String[][] accounts={{"101","Caja y efectivo","ACTIVO"},{"104","Cuentas bancarias y billeteras","ACTIVO"},{"121","Cuentas por cobrar comerciales","ACTIVO"},{"141","Préstamos y adelantos al personal","ACTIVO"},{"201","Insumos y suministros","ACTIVO"},{"331","Activos fijos","ACTIVO"},{"391","Depreciación acumulada","ACTIVO"},{"4011","IGV por pagar / crédito fiscal","PASIVO"},{"4017","Retenciones de renta","PASIVO"},{"403","Aportes y pensiones por pagar","PASIVO"},{"421","Cuentas por pagar","PASIVO"},{"461","Anticipos de clientes","PASIVO"},{"501","Capital y saldos iniciales","PATRIMONIO"},{"621","Remuneraciones y beneficios","GASTO"},{"627","Aportes del empleador","GASTO"},{"631","Servicios y gastos operativos","GASTO"},{"659","Otros gastos y mermas","GASTO"},{"681","Depreciación del período","GASTO"},{"691","Costo de ventas","COSTO"},{"701","Ventas de alimentos y servicios","INGRESO"},{"759","Intereses de préstamos","INGRESO"}};
 for(var a:accounts)db.insert("accounts",map("code",a[0],"name",a[1],"type",a[2],"nature",List.of("ACTIVO","GASTO","COSTO").contains(a[2])&&!a[0].equals("391")?"DEUDORA":"ACREEDORA","postable","SI","status","ACTIVO"));
 db.exec("INSERT INTO document_sequences(series,next_value) VALUES ('B001',1),('F001',1)");
 db.insert("warehouses",map("name","Almacén principal","location","Cocina central","category","INSUMOS","manager","Responsable de almacén","status","ACTIVO"));
 String[][] cash={{"Caja del restaurante","EFECTIVO"},{"Caja chica","EFECTIVO"},{"Cuenta bancaria","BANCO"},{"Yape del negocio","YAPE"},{"Tarjetas POS","TARJETA"}};for(var c:cash)db.insert("cash_accounts",map("name",c[0],"kind",c[1],"balance",0,"status","ACTIVO"));
 if(!demo)return;
 db.insert("partners",map("kind","PROVEEDOR","name","Distribuidora Valle del Mantaro (demo)","document","20000000002","phone","900000001","address","Huancayo","status","ACTIVO"));
 db.insert("partners",map("kind","CLIENTE","name","Cliente de prueba","document","00000000","phone","900000002","status","ACTIVO"));
 String[][] products={{"I001","Trucha fresca","KG","20","25"},{"I002","Arroz","KG","4.5","40"},{"I003","Papa amarilla","KG","3.2","35"},{"I004","Pollo","KG","10","25"},{"I005","Aceite vegetal","L","8","15"},{"I006","Verduras mixtas","KG","5","20"},{"I007","Carne de res","KG","28","12"},{"I008","Maíz morado","KG","6","10"},{"I009","Azúcar","KG","4","15"},{"I010","Limón","KG","5.5","8"}};
 for(var p:products){long product=db.insert("products",map("code",p[0],"name",p[1],"category","INSUMO","unit",p[2],"tax_type","GRAVADO","tax_rate",18,"average_cost",0,"status","ACTIVO"));var amount=inventory.move(product,1,"INGRESO",new BigDecimal(p[4]),new BigDecimal(p[3]),today(),"APERTURA-DEMO","Saldo inicial de demostración");ledger.simple("201","501",amount,"Inventario inicial demo","APERTURA-DEMO");db.insert("stock_rules",map("product_id",product,"warehouse_id",1,"supplier_id",1,"minimum",p[0].equals("I001")?22:5,"maximum",60));}
 String[][] dishes={{"P001","Trucha frita","Platos de fondo","32","Trucha dorada con arroz, papas y ensalada."},{"P002","Lomo saltado","Platos de fondo","35","Salteado al wok, papas crujientes y arroz."},{"P003","Pollo a la plancha","Platos de fondo","26","Pechuga jugosa con guarnición fresca."},{"P004","Chicharrón de trucha","Especialidades","38","Bocados de trucha con limón y papas."},{"P005","Arroz con pollo","Platos de fondo","24","Sabor de casa con verduras de temporada."},{"P006","Chicha morada","Bebidas","8","Preparación de la casa. Vaso personal."}};
 for(var p:dishes)db.insert("dishes",map("code",p[0],"name",p[1],"category",p[2],"price",new BigDecimal(p[3]),"tax_type","GRAVADO","tax_rate",18,"description",p[4],"status","ACTIVO"));
 Object[][] recipes={{1,1,"0.3"},{1,2,"0.15"},{1,3,"0.2"},{1,5,"0.03"},{1,6,"0.1"},{2,7,"0.25"},{2,2,"0.15"},{2,3,"0.2"},{2,6,"0.1"},{3,4,"0.3"},{3,2,"0.15"},{3,6,"0.15"},{4,1,"0.35"},{4,3,"0.2"},{4,5,"0.04"},{5,4,"0.25"},{5,2,"0.2"},{5,6,"0.1"},{6,8,"0.05"},{6,9,"0.025"},{6,10,"0.015"}};
 for(var r:recipes)db.insert("recipes",map("dish_id",r[0],"product_id",r[1],"quantity",r[2],"process","Preparar según la ficha técnica del restaurante"));
 for(String area:List.of("Salón principal","Terraza","Jardín")){long id=db.insert("areas",map("name",area,"max_tables",10));for(int n=1;n<=4;n++)db.insert("dining_tables",map("name",area.substring(0,1)+String.format("%02d",n),"area_id",id,"capacity",n==4?6:4,"status","LIBRE"));}
 db.insert("budgets",map("code","PRE-COCINA","area","Cocina","category","Insumos","start_date",today().withDayOfMonth(1),"end_date",today().plusMonths(3).withDayOfMonth(1).minusDays(1),"amount",15000,"executed",0,"manager","Administración"));
 long employee=db.insert("employees",map("code","E001","name","María","last_name","Quispe (demo)","document","00000001","birth_date","1995-05-10","address","Huancayo","marital_status","SOLTERO","insurance","EsSalud","status","ACTIVO"));
 db.insert("contracts",map("employee_id",employee,"start_date",today().minusYears(2),"end_date",today().plusYears(1),"position","Cocinera","regime","PEQUENA_EMPRESA","salary",1800,"vacation_days",15,"bonus_factor",0.5,"cts_factor",0.5,"pension_rate",13,"health_rate",9,"status","ACTIVO"));
 BigDecimal[] amounts={new BigDecimal("300"),new BigDecimal("500"),new BigDecimal("12000"),new BigDecimal("850"),BigDecimal.ZERO};for(int i=0;i<amounts.length;i++)if(amounts[i].signum()>0){ledger.cash(i+1,amounts[i],"INGRESO","Saldo inicial demo","APERTURA-DEMO","");ledger.simple(ledger.cashCode(i+1),"501",amounts[i],"Saldo inicial demo","APERTURA-DEMO");}
 db.insert("discounts",map("code","TUMI10","percent",10,"start_date",today().minusDays(1),"end_date",today().plusYears(1),"status","ACTIVO"));
 db.insert("audit",map("username","sistema","action","INICIALIZACION","entity","company","entity_id",1,"detail","Datos de demostración cargados una sola vez"));
 }
}
