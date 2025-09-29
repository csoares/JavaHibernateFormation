package com.formation.hibernate.entity;

import jakarta.persistence.*;

/**
 * 🎓 USER - Entidade Simples para Demonstração N+1
 * 
 * Esta entidade foi SIMPLIFICADA ao máximo para focar no problema N+1:
 * ✅ Apenas campos essenciais (id, name, email)
 * ✅ Um relacionamento LAZY com Department
 * ✅ EntityGraph para demonstrar solução
 * ✅ Sem complexidades desnecessárias
 * 
 * 🎯 FOCO: Demonstrar como user.getDepartment().getName() dispara query extra
 */

@Entity
@Table(name = "users")
// ✅ EntityGraph para resolver problema N+1
@NamedEntityGraph(
    name = "User.withDepartment",
    attributeNodes = @NamedAttributeNode("department")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // ✅ Relacionamento LAZY (padrão) - ESSENCIAL para demonstrar N+1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Construtores
    public User() {}

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public User(String name, String email, Department department) {
        this.name = name;
        this.email = email;
        this.department = department;
    }

    // Getters e Setters básicos
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // 🎯 MÉTODO CHAVE: Acesso que dispara o problema N+1
    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}