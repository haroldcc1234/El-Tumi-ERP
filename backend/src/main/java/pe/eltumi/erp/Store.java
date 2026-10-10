package pe.eltumi.erp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import java.sql.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Component
public class Store {
 public final JdbcTemplate jdbc; public final ObjectMapper json;
 public Store(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
 public List<Map<String,Object>> rows(String sql,Object... args){return jdbc.query(sql,(rs,i)->{Map<String,Object> m=new LinkedHashMap<>(); var meta=rs.getMetaData(); for(int c=1;c<=meta.getColumnCount();c++){Object v=rs.getObject(c); if(v instanceof Clob clob)v=clob.getSubString(1,(int)clob.length()); if(v instanceof java.sql.Date x)v=x.toLocalDate().toString(); if(v instanceof Timestamp x)v=x.toLocalDateTime().toString(); m.put(meta.getColumnLabel(c).toLowerCase(Locale.ROOT),v);}return m;},args);}
 public Map<String,Object> one(String sql,Object... args){var r=rows(sql,args);if(r.isEmpty())throw new Problem(404,"Registro no encontrado");return r.get(0);}
 public Map<String,Object> get(String table,long id){return one("SELECT * FROM "+table+" WHERE id=?",id);}
 public Map<String,Object> lock(String table,long id){return one("SELECT * FROM "+table+" WHERE id=? FOR UPDATE",id);}
 public int exec(String sql,Object...args){return jdbc.update(sql,args);}
 public long count(String sql,Object...args){Long v=jdbc.queryForObject(sql,Long.class,args);return v==null?0:v;}
 public BigDecimal sum(String sql,Object...args){BigDecimal v=jdbc.queryForObject(sql,BigDecimal.class,args);return v==null?BigDecimal.ZERO:v;}
 public long insert(String table,Map<String,Object> values){String cols=String.join(",",values.keySet());String qs=String.join(",",Collections.nCopies(values.size(),"?"));KeyHolder h=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO "+table+" ("+cols+") VALUES ("+qs+")",new String[]{"id"});int i=1;for(Object v:values.values())ps.setObject(i++,v);return ps;},h);return Objects.requireNonNull(h.getKey()).longValue();}
 public void update(String table,long id,Map<String,Object> values){if(values.isEmpty())return;var set=new ArrayList<String>();var args=new ArrayList<Object>();values.forEach((k,v)->{set.add(k+"=?");args.add(v);});args.add(id);exec("UPDATE "+table+" SET "+String.join(",",set)+",version=version+1 WHERE id=?",args.toArray());}
 public static LinkedHashMap<String,Object> map(Object... pairs){var m=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)m.put(pairs[i].toString(),pairs[i+1]);return m;}
 public static String str(Map<String,?> m,String k){Object v=m.get(k);return v==null?"":v.toString();}
 public static long id(Map<String,?> m,String k){Object v=m.get(k);if(v==null||v.toString().isBlank())return 0;return Long.parseLong(v.toString());}
 public static BigDecimal dec(Map<String,?> m,String k){Object v=m.get(k);return v==null||v.toString().isBlank()?BigDecimal.ZERO:new BigDecimal(v.toString());}
 public static BigDecimal money(BigDecimal x){return x.setScale(2,RoundingMode.HALF_UP);}
 public static LocalDate today(){return LocalDate.now(ZoneId.of("America/Lima"));}
 public static void require(boolean ok,String message){if(!ok)throw new Problem(422,message);}
 public static void positive(BigDecimal d){require(d.signum()>0,"El monto o la cantidad debe ser mayor que cero");}
 public String json(Object x){try{return json.writeValueAsString(x);}catch(Exception e){throw new IllegalArgumentException(e);}}
 public List<Map<String,Object>> parseLines(String x){try{return json.readValue(x,new TypeReference<>(){});}catch(Exception e){throw new Problem(422,"Detalle inválido");}}
 @SuppressWarnings("unchecked") public static List<Map<String,Object>> lines(Map<String,Object> b){Object x=b.get("lines");require(x instanceof List<?>,"Agrega por lo menos una línea");List<Map<String,Object>> l=(List<Map<String,Object>>)x;require(!l.isEmpty()&&l.size()<=200,"Se requieren entre 1 y 200 líneas");return l;}
}
