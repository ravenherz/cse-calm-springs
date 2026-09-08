# Roles

**Roles** is who may use the public site and who may open the editor. There is exactly one owner. Transfer it from the owner row; that account becomes **Admin**. Only **Admin** and **Owner** open the editor.

![Access matrix: public site vs editor](roles-md-image-1.jpg)

## Access

The matrix is documentation of the live rules:

| Surface | Guest | Member | Moderator | Admin | Owner |
| --- | --- | --- | --- | --- | --- |
| Public site | yes | yes | yes | yes | yes |
| Editor `/editor` | — | — | — | yes | yes |

Everyone can read the public site. The desk is for operators.

## Accounts on this page

Each non-owner row has a **Role** list and **Save**. Assignable levels depend on who you are; you cannot promote past your own reach.

The owner row does not use that list. If you are allowed to transfer, it shows **Transfer to…** and a target. After transfer, the former owner is **Admin**.

Your own row is marked **you**.

## After a change

**Save** writes the role. A success line confirms it. The person must meet **Admin** or **Owner** to keep using Catalog; anyone else is signed out of the editor on the next request.
