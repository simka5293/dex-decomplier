package com.wendal.java.dex.decomplier.javafile.model;

import java.util.ArrayList;

import com.wendal.java.dex.decomplier.dexfile.model.Dex_Method;
import com.wendal.java.dex.decomplier.dexfile.model.Dex_Method.LocalVar;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Check_Cast;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Const;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Goto;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_If;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Instance_Of;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Invoke_Direct;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Invoke_Static;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Move;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Move_Result;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_ReturnX;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_ReturnVoid;
import com.wendal.java.dex.decomplier.javafile.model.statement.PrototypeStatement_Throw;
import com.wendal.java.dex.decomplier.toolkit.String_Toolkit;

public class JavaMethod {

    /**
     * public , private , protected , or ""
     */
    public String access_flags = DEFAUFT;

    public boolean isStatic = false;
    public boolean isFinal = false;
    public boolean isAbstract = false;

    public String name;

    public static String PUBLIC = "public";
    public static String PRIVATE = "private";
    public static String PROTECTED = "protected";
    public static String DEFAUFT = "";

    public String return_value = "void";

    public ArrayList<String> parameter_list = new ArrayList<String>();

    public ArrayList<String> src_code = new ArrayList<String>(100);

    private Dex_Method dex_method;

    private boolean isStaticConstructor = false;
    
    private ArrayList<PrototypeStatement> ps_list;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(access_flags).append(" ");
        if (isStatic) {
            sb.append("static").append(" ");
        }
        if (isFinal) {
            sb.append("final").append(" ");
        }
        sb.append(return_value).append(" ");
        if (isStaticConstructor) {
            ;
        } else {
            sb.append(name);
            sb.append("(");
            for (int i = parameter_list.size() - 1; i > -1; i--) {
                sb.append(parameter_list.get(i));
                if (i > 0) {
                    sb.append(" ,");
                }
            }
            sb.append(")");
        }
        sb.append("\n");
        if (isAbstract) {
            ;
        } else {
            sb.append("{\n");
//            for (String str : src_code) {
//                sb.append(str).append(";\n");
//            }
            for (PrototypeStatement ps : ps_list) {
                sb.append(ps.toString()).append(";\n");
            }
            sb.append("}\n");
        }
        return sb.toString();
    }

    public JavaMethod(Dex_Method dex_method) {
        this.dex_method = dex_method;
    }

    public void parse() {
        // ��������
        this.name = this.dex_method.getName();
        // �������Ʒ�
        String access_flag = this.dex_method.getAccess_flag();
        if (access_flag.indexOf("ABSTRACT") > -1) {
            this.isAbstract = true;
        } else {
            if (access_flag.indexOf("FINAL") > -1) {
                this.isFinal = true;
            }
            if (access_flag.indexOf("STATIC") > -1) {
                this.isStatic = true;
            }
        }

        // �������ʿ��Ʒ�
        if (access_flag.indexOf("PUBLIC") > -1) {
            this.access_flags = PUBLIC;
        } else if (access_flag.indexOf("PRIVATE") > -1) {
            this.access_flags = PRIVATE;
        } else if (access_flag.indexOf("PROTECTED") > -1) {
            this.access_flags = PROTECTED;
        }

        // ��ȡ����ֵ
        {
            String src_type = this.dex_method.getType();
            String tmp_type = src_type.substring(src_type.indexOf(")") + 1);
            if (tmp_type.equals("V")) {
                ;
            } else {
                this.return_value = String_Toolkit.parseType(tmp_type)
                        .replaceAll(";", "");
            }
            // �������<init>,���ʾ��Ϊ���췽��,���Ϊ<clinit>��Ϊ��̬��
            if (this.name.indexOf("<init>") > -1) {
                this.return_value = "";
            } else if (this.name.indexOf("<clinit>") > -1) {
                this.return_value = "";
                this.isStaticConstructor = true;
            }
        }
        // ����parameter�б�
        {
            String src_type = this.dex_method.getType();
            String tmp_type = src_type.substring(1, src_type.indexOf(")"))
                    .trim();

            if (tmp_type.length() < 1) {
                ;// ��ʾ�÷�����û�д�������
            } else {
                // System.out.println("------------------------->>> "+
                // tmp_type);
                ArrayList<LocalVar> cv_list = this.dex_method.getLocals_list();
                String tmp_str = "";
                int count_parameter = 0;
                for (int i = cv_list.size() - 1; i > -1; i--) {
                    tmp_str = cv_list.get(i).src_name + tmp_str;
                    count_parameter++;
                    String parameter_type = String_Toolkit.parseType(
                            cv_list.get(i).type).replaceAll(";", "");
                    String parameter = parameter_type + " "
                            + cv_list.get(i).name;
                    parameter_list.add(parameter);
                    if (tmp_str.equals(tmp_type)) {
                        break;
                    }
                }
            }
        }
        parseOpcode();
    }
    private void parseOpcode() {
        ArrayList<String> opcode_src = this.dex_method.getOpcodes_list();
        opcode_src.remove(0);//ȥ����һ��,������ǩ��
        
        
        ps_list = PrototypeStatement.newInstanceList(opcode_src);
        //�Ǽ�ȫ��return-void���,ֻ��¼line_index
        ArrayList<String> ps_returnvoid_list = new ArrayList<String>();
        for (PrototypeStatement ps : ps_list) {
            if (ps instanceof PrototypeStatement_ReturnVoid) {
                ps_returnvoid_list.add(ps.line_index);
            }
        }
        //�滻��ָ��return-void��goto���
        //System.out.println(ps_returnvoid_list.size());
        for (int i = 0;i < ps_list.size();i++) {
            PrototypeStatement ps = ps_list.get(i);
            if (ps instanceof PrototypeStatement_Goto) {
                PrototypeStatement_Goto ps_goto = (PrototypeStatement_Goto)ps;
                for (String string : ps_returnvoid_list) {
                    if(string.equals(ps_goto.goto_line_index)){
                        //�滻��return-void
                        int index = ps_list.indexOf(ps);
                        ps_list.set(index, new PrototypeStatement_ReturnVoid(ps.line_index));
                        
//                        System.out.println("--->�滻��תreturn-void: " + ps_goto.line_index+" --> "+ps_goto.goto_line_index);
                        break;
                    }
                }
            }
        }
        
        for (int i = 0;i < ps_list.size();i++) {
            PrototypeStatement ps = ps_list.get(i);
            
            if(ps instanceof PrototypeStatement_ReturnVoid){
                continue;
            }
            
            if(ps instanceof PrototypeStatement_Goto){
                continue;
            }

            //����Return-X
            if(ps.opcodes.startsWith(OpCode_List.Op_Return_Object) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Return_V) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Return_Wide)){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps, PrototypeStatement_ReturnX.class));
                continue;
            }
            
           //����CheckCase
            if(ps.opcodes.startsWith(OpCode_List.Op_CheckCase)){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps,PrototypeStatement_Check_Cast.class));
                continue;
            }
            
            //����Const
            if(ps.opcodes.startsWith(OpCode_List.Op_Conset4) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Conset16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Conset_V) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Conset_high16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Conset_wide16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Conset_wide32) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Conset_wide) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Conset_wide_high16)){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps, PrototypeStatement_Const.class));
                continue;
            }
            
            
            if(ps.opcodes.startsWith(OpCode_List.Op_Invoke_Static)){
                ps_list.set(ps_list.lastIndexOf(ps), new PrototypeStatement_Invoke_Static(ps));
                continue;
            }
            
            if(ps.opcodes.startsWith(OpCode_List.Op_Invoke_Direct)){
                ps_list.set(ps_list.lastIndexOf(ps), new PrototypeStatement_Invoke_Direct(ps));
                continue;
            }
            //��ʱʹ��Invoke_Direct����Invoke_Virtual
            if(ps.opcodes.startsWith(OpCode_List.Op_Invoke_Virtual)){
                ps_list.set(ps_list.lastIndexOf(ps), new PrototypeStatement_Invoke_Direct(ps));
                continue;
            }
            //��ʱʹ��Invoke_Direct����Invoke_Interface
            if(ps.opcodes.startsWith(OpCode_List.Op_Invoke_Interface)){
                ps_list.set(ps_list.lastIndexOf(ps), new PrototypeStatement_Invoke_Direct(ps));
                continue;
            }
            
            //����Move-Result
            if(ps.opcodes.startsWith(OpCode_List.Op_Move_Result) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_Result_Wide) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_Result_Object)){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps, PrototypeStatement_Move_Result.class));
                continue;
            }
            
            //����Move
            if(ps.opcodes.startsWith(OpCode_List.Op_Move) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_from16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_wide) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_wide_from16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_wide_16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_object) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_object_16) ||
                    ps.opcodes.startsWith(OpCode_List.Op_Move_object_from16)){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps, PrototypeStatement_Move.class));
                continue;
            }
            
            //����instance of
            if(ps.opcodes.startsWith(OpCode_List.Op_Instance_of)){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps, PrototypeStatement_Instance_Of.class));
                continue;
            }
            
            //����Throw
            if(ps.opcodes.startsWith(OpCode_List.Op_Throw)){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps, PrototypeStatement_Throw.class));
                continue;
            }
            
            //����if
            if(ps.opcodes.startsWith("3")){
                String tmp_str = ps.opcodes.substring(0,2);
                int op_int = Integer.parseInt(tmp_str, 16);
                if(op_int >= 0x32 && op_int <= 0x3d){
                    ps_list.set(ps_list.lastIndexOf(ps), new PrototypeStatement_If(ps));
                    continue;
                }
            }
            
            //����sget
            if(ps.opcodes.startsWith(OpCode_List.Op_sget) ||
                    ps.opcodes.startsWith(OpCode_List.Op_sget_wide) ||
                    ps.opcodes.startsWith(OpCode_List.Op_sget_object) ||
                    ps.opcodes.startsWith(OpCode_List.Op_sget_short) ||
                    ps.opcodes.startsWith(OpCode_List.Op_sget_boolean) ||
                    ps.opcodes.startsWith(OpCode_List.op_sget_byte) ||
                    ps.opcodes.startsWith(OpCode_List.Op_sget_char) ||
                    ps.opcodes.startsWith(OpCode_List.Op_sget_char) ){
                ps_list.set(ps_list.lastIndexOf(ps), PrototypeStatement.convertTotype(ps, PrototypeStatement_Move.class));
                continue;
            }
            
        }
        
//        //Ѱ���½�����
//        if(this.name.equals("<init>")){
//            return;
//        }
//        
//        int new_i = 0;
//        int init_i = 0;
//        for (int i = 0;i < ps_list.size();i++) {
//            PrototypeStatement ps = ps_list.get(i);
//            if(ps.opcodes == null) continue;
//            if(ps.opcodes.startsWith("22")) new_i++;
//            if(ps instanceof PrototypeStatement_Invoke_Direct){
//                if(((PrototypeStatement_Invoke_Direct) ps).method_name.equals("<init>")) init_i++;
//            }
//        }
//        System.out.println("----> "+new_i+"  -->"+init_i +"  Eq ?---->>>>" + (new_i == init_i));
    }
    
    
}