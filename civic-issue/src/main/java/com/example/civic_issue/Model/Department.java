package com.example.civic_issue.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;           // "Water & Sewerage", "Electricity & Power", etc.
    private String description;    // Department description
    private String address;        // Department address

    // Head of the department
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "head_id")
    @JsonIgnoreProperties({"assignedComplaints", "password"})
    private User head;

    // Operators under this department
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<User> operators;

    // Optional: convenience method to get operator count
    public int getOperatorCount() {
        return operators != null ? operators.size() : 0;
    }
}
