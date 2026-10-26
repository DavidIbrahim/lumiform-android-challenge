package com.davidsalama.lumiform.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per form item, for every type (single-table inheritance).
 *
 * The tree is stored as an adjacency list: each row points to its parent and keeps its
 * [position] among its siblings, since SQL rows have no order of their own.
 * Columns that only some types use are nullable.
 */
@Entity(
    tableName = "form_node",
    foreignKeys = [
        ForeignKey(
            entity = FormNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parent_id")],
)
data class FormNodeEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "parent_id") val parentId: Long?,
    val position: Int,
    val type: NodeType,
    /** Page, section and image title. */
    val title: String? = null,
    /** Text and choice question text. */
    val content: String? = null,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "response_set_id") val responseSetId: Long? = null,
    @ColumnInfo(name = "multiple_selection") val multipleSelection: Boolean? = null,
    /** The server's `type` string, kept only for [NodeType.UNKNOWN]. */
    @ColumnInfo(name = "raw_type") val rawType: String? = null,
)

enum class NodeType { PAGE, SECTION, TEXT, IMAGE, CHOICE, UNKNOWN }
